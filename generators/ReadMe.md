Here are small programs that generate some parts of source code in kotlin project, such as repetitive stdlib functions for all primitive arrays, or protobuf classes used by compiler.

Only some of them are run automatically when building the compiler, others you need to invoke manually (e.g. `./gradlew generateTests`).
Generated sources should generally be committed to the repository.