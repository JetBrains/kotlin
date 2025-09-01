plugins {
    java
}

repositories {
    mavenCentral()
}

val protobufVersion: String by rootProject.extra

dependencies {
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
}

plugins.withId("java-base") {
    extensions.getByType<JavaPluginExtension>().toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}
