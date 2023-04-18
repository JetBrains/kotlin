## Build Tools API

This module contains public interfaces for Kotlin build tools. 
Using APIs from this module should be the preferred way to work with Kotlin compiler when integrating Kotlin builds into different build systems.
The Kotlin stdlib of at least Kotlin 1.4 is expected to be a dependency of a consumer of the API. 

The default implementation of the API is located in the [kotlin-build-tools-impl](../kotlin-build-tools-impl) directory.
Interfaces implementation are expected to be loaded using the [ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).
The purpose of such a segregation is to allow using this API with different Kotlin compiler versions. 