apply { plugin("kotlin") }

dependencies {
    compile(project(":idea"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:frontend.java"))
//    compileOnly(intellijDep()) { includeJars("annotations", "openapi", "idea", "extensions", "util", "velocity", "boot", "gson",
//                                             "swingx-core", "forms_rt", "jdom", "log4j", "guava", "asm-all", rootProject = rootProject) }
    intellijKompotDep(project)
    compileOnly(commonDep("com.google.code.findbugs", "jsr305"))

//    compileOnly(intellijPluginDep("junit")) { includeJars("idea-junit") }
//    compileOnly(intellijPluginDep("testng")) { includeJars("testng", "testng-plugin") }
//    compileOnly(intellijPluginDep("coverage")) { includeJars("coverage") }
//    compileOnly(intellijPluginDep("java-decompiler")) { includeJars("java-decompiler") }
//    compileOnly(intellijPluginDep("IntelliLang"))
//    compileOnly(intellijPluginDep("copyright"))
//    compileOnly(intellijPluginDep("properties"))
//    compileOnly(intellijPluginDep("java-i18n"))
    intellijPluginKompotDep(project, "junit")
    intellijPluginKompotDep(project, "testng")
    intellijPluginKompotDep(project, "coverage")
    intellijPluginKompotDep(project, "java-decompiler")
    intellijPluginKompotDep(project, "IntelliLang")
    intellijPluginKompotDep(project, "copyright")
    intellijPluginKompotDep(project, "properties")
    intellijPluginKompotDep(project, "java-i18n")

}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

configureInstrumentation()
configureVerification()