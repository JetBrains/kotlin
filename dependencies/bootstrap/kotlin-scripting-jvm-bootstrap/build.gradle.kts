val resolvedBootstrap = configurations.resolvable("kotlinScriptingJvmBootstrapClasspath") {
    dependencies.addLater(providers.provider {
        project.dependencies.create("org.jetbrains.kotlin:kotlin-scripting-jvm:${bootstrapKotlinVersion}")
    })
}

configurations.consumable("kotlinScriptingJvmRuntimeElements") {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
    outgoing.artifacts(resolvedBootstrap)
}
