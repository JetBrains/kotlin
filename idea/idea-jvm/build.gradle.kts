
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":idea"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:frontend.java"))
    compileOnly(intellijDep())
    compileOnly(commonDep("com.google.code.findbugs", "jsr305"))

    compileOnly(intellijPluginDep("junit"))
    compileOnly(intellijPluginDep("testng"))
    compileOnly(intellijPluginDep("coverage"))
    compileOnly(intellijPluginDep("java-decompiler"))
    compileOnly(intellijPluginDep("IntelliLang"))
    compileOnly(intellijPluginDep("copyright"))
    compileOnly(intellijPluginDep("properties"))
    compileOnly(intellijPluginDep("java-i18n"))
    compileOnly(intellijPluginDep("stream-debugger"))
}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

configureInstrumentation()

runtimeJar {
    archiveName = "jvm-ide.jar"
}

ideaPlugin()
