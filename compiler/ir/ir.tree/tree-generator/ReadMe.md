# IR tree generator

This module generates the IR tree interfaces and classes, as well as visitors and transformers.

The generator is run on every build. If you change something in the model or in the generator code, just run `./gradlew dist` and the generator task will run before the start of the compilation.

The model is declared in [IrTree.kt](src/org/jetbrains/kotlin/ir/generator/IrTree.kt).

You can navigate to the model from a generated `Ir*` class by following the `Generated from` link in its kdoc.
