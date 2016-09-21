# Kotlin-native interop

## Usage

Create a Gradle subproject somewhere under `../`, using `../InteropExample` as a template.

To generate the interop stubs and libraries you can run the following command from `../`:

    ./gradlew InteropExample:build

To run the example (if 'application' plugin is enabled):

    ./gradlew InteropExample:run
