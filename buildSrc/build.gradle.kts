buildscript {
    configure(listOf(repositories, project.repositories)) {
        gradleScriptKotlin()
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin"))
    }
}

apply {
    plugin("kotlin")
}

dependencies {
    compile(gradleScriptKotlinApi())
}
