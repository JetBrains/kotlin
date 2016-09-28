# Kotlin-native backend #

## Build

First, build Kotlin IR branch tree with:
	pushd backend.native/kotlin-ir
	ant -f ./update_dependencies.xml jb_update
	ant -f ./build.xml
	popd

To build native translator just use:
	gradle :backend.native:run

And it will run simple example (currently prints out IR of test file).
