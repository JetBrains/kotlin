plugins {
    kotlin("jvm")
}

dependencies {
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
}


