
apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

dependencies {
    compile(project(":idea"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:frontend.java"))
    compileOnly(intellijDep()) { includeJars("annotations", "openapi", "idea", "platform-api", "platform-impl", "java-api",
                                             "java-impl", "extensions", "util", "bootstrap", "gson",
                                             "swingx-core", "forms_rt", "jdom", "log4j", "guava", "asm-all", "picocontainer",
                                             rootProject = rootProject) }
    compileOnly(commonDep("com.google.code.findbugs", "jsr305"))

    compileOnly(intellijPluginDep("junit")) { includeJars("idea-junit") }
    compileOnly(intellijPluginDep("testng")) { includeJars("testng", "testng-plugin") }
    compileOnly(intellijPluginDep("coverage")) { includeJars("coverage") }
    compileOnly(intellijPluginDep("java-decompiler")) { includeJars("java-decompiler") }
    compileOnly(intellijPluginDep("IntelliLang"))
    compileOnly(intellijPluginDep("copyright"))
    compileOnly(intellijPluginDep("properties"))
    compileOnly(intellijPluginDep("java-i18n"))
}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

configureInstrumentation()
