val resolvedBootstrap = configurations.resolvable("kotlinBuildToolsApiBootstrapClasspath") {
    dependencies.addLater(providers.provider {
        project.dependencies.create("org.jetbrains.kotlin:kotlin-build-tools-api:${bootstrapKotlinVersion}")
    })
}

val outgoingBootstrap = configurations.consumable("buildToolsApiElements") {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
}

artifacts {
    add(
        outgoingBootstrap.name,
        resolvedBootstrap.map { it.resolve().single() }
    )
}