plugins {
    kotlin("jvm")
}

dependencies {
    compile(kotlin("stdlib"))
    compile(project(":idea")) { isTransitive = false }
    compile(project(":idea:idea-gradle")) { isTransitive = false }
    compile(project(":kotlin-ultimate:ide:common-noncidr-native"))
    compileOnly(intellijDep()) { includeJars("external-system-rt", "idea", "platform-api", "util") }
}


the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}

val jarTask = (tasks.findByName("jar") as Jar? ?: task<Jar>("jar")).apply {
    val jarTask = project(":idea:idea-gradle").tasks.findByName("jar")!!
    dependsOn(jarTask)
    from(zipTree(jarTask.outputs.files.singleFile)) {
        include("org")
    }

    archiveFileName.set("mobile-mpp.jar")
}

