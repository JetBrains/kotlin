@file:JvmName("JvmToolchain")
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.*
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

enum class JdkMajorVersion(
    val majorVersion: Int,
    val targetName: String = majorVersion.toString(),
    val overrideMajorVersion: Int? = null,
    private val mandatory: Boolean = true
) {
    JDK_1_6(6, targetName = "1.6", overrideMajorVersion = 8),
    JDK_1_7(7, targetName = "1.7", overrideMajorVersion = 8),
    JDK_1_8(8, targetName = "1.8"),
    JDK_9(9),
    JDK_10(10, mandatory = false),
    JDK_11(11, mandatory = false),
    JDK_15(15, mandatory = false),
    JDK_16(16, mandatory = false);

    fun isMandatory(): Boolean = mandatory

    companion object {
        fun fromMajorVersion(majorVersion: Int) = values().first { it.majorVersion == majorVersion }
    }
}

fun Project.configureJvmDefaultToolchain() {
    configureJvmToolchain(JdkMajorVersion.JDK_1_8)
}

fun Project.shouldOverrideObsoleteJdk(
    jdkVersion: JdkMajorVersion
): Boolean = kotlinBuildProperties.isObsoleteJdkOverrideEnabled &&
        jdkVersion.overrideMajorVersion != null

fun Project.configureJvmToolchain(
    jdkVersion: JdkMajorVersion
) {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        val kotlinExtension = extensions.getByType<KotlinTopLevelExtension>()

        if (shouldOverrideObsoleteJdk(jdkVersion)) {
            kotlinExtension.jvmToolchain {
                (this as JavaToolchainSpec).languageVersion
                    .set(JavaLanguageVersion.of(jdkVersion.overrideMajorVersion!!))
            }
            updateJvmTarget(jdkVersion.targetName)
        } else {
            kotlinExtension.jvmToolchain {
                (this as JavaToolchainSpec).languageVersion
                    .set(JavaLanguageVersion.of(jdkVersion.majorVersion))
            }
        }

        tasks
            .matching { it.name != "compileJava9Java" && it is JavaCompile }
            .configureEach {
                with(this as JavaCompile) {
                    options.compilerArgs.add("-proc:none")
                    options.encoding = "UTF-8"
                }
            }

        tasks.withType<KotlinCompile>().configureEach {
            kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
        }
    }
}

fun Project.configureJavaOnlyToolchain(
    jdkVersion: JdkMajorVersion
) {
    plugins.withId("java-base") {
        val javaExtension = extensions.getByType<JavaPluginExtension>()
        if (shouldOverrideObsoleteJdk(jdkVersion)) {
            javaExtension.toolchain {
                languageVersion.set(
                    JavaLanguageVersion.of(jdkVersion.overrideMajorVersion!!)
                )
            }
            tasks.withType<JavaCompile>().configureEach {
                targetCompatibility = jdkVersion.targetName
                sourceCompatibility = jdkVersion.targetName
            }
        } else {
            javaExtension.toolchain {
                languageVersion.set(
                    JavaLanguageVersion.of(jdkVersion.majorVersion)
                )
            }
        }
    }
}

fun KotlinCompile.configureTaskToolchain(
    jdkVersion: JdkMajorVersion
) {
    if (project.shouldOverrideObsoleteJdk(jdkVersion)) {
        kotlinJavaToolchain.toolchain.use(
            project.getToolchainLauncherFor(
                JdkMajorVersion.fromMajorVersion(
                    jdkVersion.overrideMajorVersion!!
                )
            )
        )
        kotlinOptions {
            jvmTarget = jdkVersion.targetName
        }
    } else {
        kotlinJavaToolchain.toolchain.use(
            project.getToolchainLauncherFor(jdkVersion)
        )
    }
}

fun JavaCompile.configureTaskToolchain(
    jdkVersion: JdkMajorVersion
) {
    if (project.shouldOverrideObsoleteJdk(jdkVersion)) {
        javaCompiler.set(
            project.getToolchainCompilerFor(
                JdkMajorVersion.fromMajorVersion(
                    jdkVersion.overrideMajorVersion!!
                )
            )
        )
        targetCompatibility = jdkVersion.targetName
        sourceCompatibility = jdkVersion.targetName
    } else {
        javaCompiler.set(project.getToolchainCompilerFor(jdkVersion))
    }
}

fun Project.updateJvmTarget(
    jvmTarget: String
) {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = jvmTarget
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = jvmTarget
        targetCompatibility = jvmTarget
    }
}

fun Project.getToolchainCompilerFor(
    jdkVersion: JdkMajorVersion
): Provider<JavaCompiler> {
    val service = project.extensions.getByType<JavaToolchainService>()
    return service.compilerFor {
        this.languageVersion.set(JavaLanguageVersion.of(jdkVersion.majorVersion))
    }
}

fun Project.getToolchainLauncherFor(
    jdkVersion: JdkMajorVersion
): Provider<JavaLauncher> {
    val service = project.extensions.getByType<JavaToolchainService>()
    val jdkVersionWithOverride = project.getJdkVersionWithOverride(jdkVersion)
    return service.launcherFor {
        this.languageVersion.set(JavaLanguageVersion.of(jdkVersionWithOverride.majorVersion))
    }
}


fun Project.getJdkVersionWithOverride(jdkVersion: JdkMajorVersion): JdkMajorVersion {
    return if (project.shouldOverrideObsoleteJdk(jdkVersion)) {
        JdkMajorVersion.fromMajorVersion(jdkVersion.overrideMajorVersion!!)
    } else {
        jdkVersion
    }
}