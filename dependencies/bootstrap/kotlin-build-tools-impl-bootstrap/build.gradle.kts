val resolvedBootstrap = configurations.resolvable("kotlinBuildToolsApiImplBootstrapClasspath") {
    val dependency: Dependency = project.dependencies.create("org.jetbrains.kotlin:kotlin-build-tools-impl:${bootstrapKotlinVersion}")
    dependencies.addLater(providers.provider { dependency })
}

configurations.consumable("buildToolsApiImplElements") {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }

    outgoing.artifacts(resolvedBootstrap)
}
