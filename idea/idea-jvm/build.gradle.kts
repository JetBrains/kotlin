
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":idea"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend.jvm"))

    compileOnly(toolsJar())
    compileOnly(intellijDep())
    compileOnly(commonDep("com.google.code.findbugs", "jsr305"))

    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java"))
    }

    compileOnly(intellijPluginDep("junit"))
    compileOnly(intellijPluginDep("testng"))
    compileOnly(intellijPluginDep("coverage"))
    compileOnly(intellijPluginDep("java-decompiler"))
    compileOnly(intellijPluginDep("IntelliLang"))
    compileOnly(intellijPluginDep("copyright"))
    compileOnly(intellijPluginDep("properties"))
    compileOnly(intellijPluginDep("java-i18n"))
}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

configureFormInstrumentation()

runtimeJar()
