buildscript {
    repositories {
        jcenter()
        mavenCentral()
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.60-eap-76")
    }
}

apply(plugin  ="kotlin")
