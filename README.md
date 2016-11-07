# Kotlin-native backend #

## Build

Download dependencies:

	gradle :dependencies:update

To run native translator just use:

	gradle :backend.native:run

And it will run simple example (currently prints out IR of test file).
For more tests, use:

	gradle :backend.native:tests:run
