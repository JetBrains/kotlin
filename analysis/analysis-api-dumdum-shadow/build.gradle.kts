import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    `maven-publish`
//    id("com.github.johnrengelman.shadow")
}


val embed by configurations.creating

dependencies {
    embed(project(":analysis:analysis-api-dumdum"))
}

val shadowTask = tasks.register<ShadowJar>("shadowJar") {
    from(embed)
    dependencies {
        exclude("kotlin/**")
        exclude("kotlinx/**")
    }

//    this.archiveClassifier.set("embeddable")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            artifact(shadowTask)
        }
    }
}
