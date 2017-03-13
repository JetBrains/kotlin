# Kotlin-native backend #

Download dependencies:

	./gradlew dependencies:update

Then build the compiler:

	./gradlew dist

After that you should be able to compile your programs like that:

	./dist/bin/konanc hello.kt -o hello

For an optimized compilation use -opt:

	./dist/bin/konanc hello.kt -o hello -opt

For some tests, use:

	./gradlew backend.native:tests:run

To run blackbox compiler tests from JVM Kotlin use (takes time):

    ./gradlew run_external

To update the blackbox compiler tests set TeamCity build number in `gradle.properties`:

    testDataVersion=<build number>:id

and run `./gradlew update_external_tests`