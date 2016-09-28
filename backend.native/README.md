# Kotlin-native backend #

## Build

To build just use:

	gradle dist ; may fail in JS
	gradle :backend.native:cli.bc:run

And it will run simple example (currently prints out IR of test file).
