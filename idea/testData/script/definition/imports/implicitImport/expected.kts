package to

val relocatedJarContents = configurations.creating {
    attributes {
        attribute(USAGE_ATTRIBUTE, JAVA_RUNTIME)
    }
}