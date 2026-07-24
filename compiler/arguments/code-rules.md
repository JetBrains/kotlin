# Set `removedVersion` instead of removing arguments

Pattern: src/org/jetbrains/kotlin/arguments/description

Instead of removing a compiler argument definition, move it to the `removed` directory
and specify the `removedVersion`.
