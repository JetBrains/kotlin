plugins {
    java
}


val protobufVersion = rootProject.extra["protobufVersion"] as String

dependencies {
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
}

plugins.withId("java-base") {
    extensions.getByType<JavaPluginExtension>().toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}
