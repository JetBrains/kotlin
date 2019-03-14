
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":idea"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend.jvm")) // TODO: fix import (workaround for jps build)
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

configureFormInstrumentation()

runtimeJar()
