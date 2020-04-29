@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.jvm.tasks.Jar
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin


private const val MAGIC_DO_NOT_CHANGE_TEST_JAR_TASK_NAME = "testJar"

fun Project.testsJar(body: Jar.() -> Unit = {}): Jar {
    val testsJarCfg = configurations.getOrCreate("tests-jar").extendsFrom(configurations["testCompile"])

    return task<Jar>(MAGIC_DO_NOT_CHANGE_TEST_JAR_TASK_NAME) {
        dependsOn("testClasses")
        pluginManager.withPlugin("java") {
            from(testSourceSet.output)
        }
        archiveClassifier.set("tests")
        body()
        project.addArtifact(testsJarCfg, this, this)
    }
}

var Project.artifactsRemovedDiagnosticFlag: Boolean
    get() = extra.has("artifactsRemovedDiagnosticFlag") && extra["artifactsRemovedDiagnosticFlag"] == true
    set(value) {
        extra["artifactsRemovedDiagnosticFlag"] = value
    }

fun Project.removeArtifacts(configuration: Configuration, task: Task) {
    configuration.artifacts.removeAll { artifact ->
        artifact.file in task.outputs.files
    }

    artifactsRemovedDiagnosticFlag = true
}

fun Project.noDefaultJar() {
    tasks.named("jar").configure {
        enabled = false
        actions = emptyList()
        configurations.forEach { cfg ->
            removeArtifacts(cfg, this)
        }
    }
}

fun <T : Task> Project.runtimeJarArtifactBy(
    task: TaskProvider<T>,
    artifactRef: Any,
    body: ConfigurablePublishArtifact.() -> Unit = {}
) {
    addArtifact("archives", task, artifactRef, body)
    addArtifact("runtimeJar", task, artifactRef, body)
    configurations.findByName("runtime")?.let {
        addArtifact(it.name, task, artifactRef, body)
    }
}

fun Project.runtimeJar(body: Jar.() -> Unit = {}): TaskProvider<Jar> = runtimeJar(getOrCreateTask("jar", body)) { }

fun <T : Jar> Project.runtimeJar(task: TaskProvider<T>, body: T.() -> Unit = {}): TaskProvider<T> {

    tasks.named<Jar>("jar").configure {
        removeArtifacts(configurations.getOrCreate("archives"), this)
    }

    task.configure {
        configurations.findByName("embedded")?.let { embedded ->
            dependsOn(embedded)
            from {
                embedded.map(::zipTree)
            }
        }
        setupPublicJar(project.the<BasePluginConvention>().archivesBaseName)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        body()
    }

    project.runtimeJarArtifactBy(task, task)

    val runtimeJar = configurations.maybeCreate("runtimeJar").apply {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }

    val javaComponent = components.findByName("java") as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["runtimeElements"]) { skip() }
    javaComponent.addVariantsFromConfiguration(runtimeJar) { }

    return task
}

fun Project.sourcesJar(body: Jar.() -> Unit = {}): TaskProvider<Jar> {
    configure<JavaPluginExtension> {
        withSourcesJar()
    }

    val sourcesJar = getOrCreateTask<Jar>("sourcesJar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier.set("sources")

        from(project.mainSourceSet.allSource)

        project.configurations.findByName("embedded")?.let { embedded ->
            from(provider {
                embedded.resolvedConfiguration
                    .resolvedArtifacts
                    .map { it.id.componentIdentifier }
                    .filterIsInstance<ProjectComponentIdentifier>()
                    .mapNotNull {
                        project(it.projectPath)
                            .findJavaPluginConvention()
                            ?.mainSourceSet
                            ?.allSource
                    }
            })
        }

        body()
    }

    addArtifact("archives", sourcesJar)
    addArtifact("sources", sourcesJar)

    return sourcesJar
}

fun Project.javadocJar(body: Jar.() -> Unit = {}): TaskProvider<Jar> {
    configure<JavaPluginExtension> {
        withJavadocJar()
    }

    val javadocTask = getOrCreateTask<Jar>("javadocJar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier.set("javadoc")
        tasks.findByName("javadoc")?.let { it as Javadoc }?.takeIf { it.enabled }?.let {
            dependsOn(it)
            from(it.destinationDir)
        }
        body()
    }

    addArtifact("archives", javadocTask)
    return javadocTask
}

fun Project.modularJar(body: Jar.() -> Unit): TaskProvider<Jar> {
    val modularJar = configurations.maybeCreate("modularJar").apply {
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        }
    }

    val modularJarTask = getOrCreateTask<Jar>("modularJar") {
        archiveClassifier.set("modular")

        body()
    }

    addArtifact("modularJar", modularJarTask)
    addArtifact("archives", modularJarTask)

    val javaComponent = components.findByName("java") as AdhocComponentWithVariants
    javaComponent.addVariantsFromConfiguration(modularJar) { }

    return modularJarTask
}


fun Project.standardPublicJars() {
    runtimeJar()
    sourcesJar()
    javadocJar()
}

fun Project.publish(moduleMetadata: Boolean = false) {
    apply<MavenPublishPlugin>()
    apply<SigningPlugin>()

    if (!moduleMetadata) {
        tasks.withType<GenerateModuleMetadata> {
            enabled = false
        }
    }

    val javaComponent = components.findByName("java") as AdhocComponentWithVariants?
    if (javaComponent != null) {
        val runtimeElements by configurations
        val apiElements by configurations

        val publishedRuntime = configurations.maybeCreate("publishedRuntime").apply {
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            }
            extendsFrom(runtimeElements)
        }

        val publishedCompile = configurations.maybeCreate("publishedCompile").apply {
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
            }
            extendsFrom(apiElements)
        }

        javaComponent.withVariantsFromConfiguration(apiElements) { skip() }

        javaComponent.addVariantsFromConfiguration(publishedCompile) { mapToMavenScope("compile") }
        javaComponent.addVariantsFromConfiguration(publishedRuntime) { mapToMavenScope("runtime") }
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("Main") {
                if (javaComponent != null) {
                    from(javaComponent)
                } else {
                    artifact(tasks["jar"])
                }

                pom {
                    packaging = "jar"
                    description.set(project.description)
                    url.set("https://kotlinlang.org/")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    scm {
                        url.set("https://github.com/JetBrains/kotlin")
                        connection.set("scm:git:https://github.com/JetBrains/kotlin.git")
                        developerConnection.set("scm:git:https://github.com/JetBrains/kotlin.git")
                    }
                    developers {
                        developer {
                            name.set("Kotlin Team")
                            organization.set("JetBrains")
                            organizationUrl.set("https://www.jetbrains.com")
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                name = "Maven"
                url = file("${project.rootDir}/build/repo").toURI()
            }
        }
    }

    configure<SigningExtension> {
        setRequired(provider {
            project.findProperty("signingRequired")?.toString()?.toBoolean()
                ?: project.property("isSonatypeRelease") as Boolean
        })

        sign(extensions.getByType<PublishingExtension>().publications["Main"])
    }

    tasks.register("install") {
        dependsOn(tasks.named("publishToMavenLocal"))
    }

    tasks.named<PublishToMavenRepository>("publishMainPublicationToMavenRepository") {
        dependsOn(project.rootProject.tasks.named("preparePublication"))
        doFirst {
            val preparePublication = project.rootProject.tasks.named("preparePublication").get()
            val username: String? by preparePublication.extra
            val password: String? by preparePublication.extra
            val repoUrl: String by preparePublication.extra

            repository.apply {
                url = uri(repoUrl)
                if (url.scheme != "file" && username != null && password != null) {
                    credentials {
                        this.username = username
                        this.password = password
                    }
                }
            }
        }
    }
}

fun Project.publishWithLegacyMavenPlugin(body: Upload.() -> Unit = {}): Upload {
    apply<plugins.PublishedKotlinModule>()

    if (artifactsRemovedDiagnosticFlag) {
        error("`publish()` should be called before removing artifacts typically done in `noDefaultJar()` or `runtimeJar()` call")
    }

    afterEvaluate {
        if (configurations.findByName("classes-dirs") != null)
            throw GradleException("classesDirsArtifact() is incompatible with publish(), see sources comments for details")
    }

    return (tasks.getByName("uploadArchives") as Upload).apply {
        body()
    }
}

fun Project.idePluginDependency(block: () -> Unit) {
    val shouldActivate = rootProject.findProperty("publish.ide.plugin.dependencies")?.toString()?.toBoolean() == true
    if (shouldActivate) {
        block()
    }
}

fun Project.publishProjectJars(projects: List<String>, libraryDependencies: List<String> = emptyList()) {
    apply<JavaPlugin>()

    val fatJarContents by configurations.creating

    dependencies {
        for (projectName in projects) {
            fatJarContents(project(projectName)) { isTransitive = false }
        }

        for (libraryDependency in libraryDependencies) {
            fatJarContents(libraryDependency)
        }
    }

    publish()

    val jar: Jar by tasks

    jar.apply {
        dependsOn(fatJarContents)

        from {
            fatJarContents.map(::zipTree)
        }
    }

    sourcesJar {
        from {
            projects.map {
                project(it).mainSourceSet.allSource
            }
        }
    }

    javadocJar()
}

fun Project.publishTestJar(projectName: String) {
    apply<JavaPlugin>()

    val fatJarContents by configurations.creating

    dependencies {
        fatJarContents(project(projectName, configuration = "tests-jar")) { isTransitive = false }
    }

    publish()

    val jar: Jar by tasks

    jar.apply {
        dependsOn(fatJarContents)

        from {
            fatJarContents.map(::zipTree)
        }
    }

    sourcesJar {
        from {
            project(projectName).testSourceSet.allSource
        }
    }

    javadocJar()
}

fun ConfigurationContainer.getOrCreate(name: String): Configuration = findByName(name) ?: create(name)

fun Jar.setupPublicJar(baseName: String, classifier: String = "") {
    val buildNumber = project.rootProject.extra["buildNumber"] as String
    this.archiveBaseName.set(baseName)
    this.archiveClassifier.set(classifier)
    manifest.attributes.apply {
        put("Implementation-Vendor", "JetBrains")
        put("Implementation-Title", baseName)
        put("Implementation-Version", buildNumber)
    }
}


fun Project.addArtifact(configuration: Configuration, task: Task, artifactRef: Any, body: ConfigurablePublishArtifact.() -> Unit = {}) {
    artifacts.add(configuration.name, artifactRef) {
        builtBy(task)
        body()
    }
}

fun Project.addArtifact(configurationName: String, task: Task, artifactRef: Any, body: ConfigurablePublishArtifact.() -> Unit = {}) =
    addArtifact(configurations.getOrCreate(configurationName), task, artifactRef, body)

fun <T : Task> Project.addArtifact(
    configurationName: String,
    task: TaskProvider<T>,
    body: ConfigurablePublishArtifact.() -> Unit = {}
): PublishArtifact {
    configurations.maybeCreate(configurationName)
    return artifacts.add(configurationName, task, body)
}

fun <T : Task> Project.addArtifact(
    configurationName: String,
    task: TaskProvider<T>,
    artifactRef: Any,
    body: ConfigurablePublishArtifact.() -> Unit = {}
): PublishArtifact {
    configurations.maybeCreate(configurationName)
    return artifacts.add(configurationName, artifactRef) {
        builtBy(task)
        body()
    }
}

fun Project.cleanArtifacts() {
    configurations["archives"].artifacts.let { artifacts ->
        artifacts.forEach {
            artifacts.remove(it)
        }
    }
}
