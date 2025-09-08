val resolvedBootstrap = configurations.resolvable("kotlinDaemonClientBootstrapClasspath") {
    dependencies.addLater(providers.provider {
        project.dependencies.create("org.jetbrains.kotlin:kotlin-daemon-client:${bootstrapKotlinVersion}")
    })
}

configurations.consumable("kotlinDaemonClientElements") {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }

    outgoing.artifacts(resolvedBootstrap)
}
