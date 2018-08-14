# Joint Kotlin Build

How to work with Kotlin joint build:

1. Switch to `native-support/master` branch: `git checkout native-support/master`
2. Download Kotlin/Native repository and place it into `kotlin-native` subdirectory:
   1. `git clone https://github.com/JetBrains/kotlin-native.git`
   2. `git checkout native-support/master`
3. Similarly for Ultimate repository - place it into `kotlin-ultimate`:
   1. `git clone ssh://git@jetbrains.team/kotlin-ultimate.git`
   2. `git checkout native-support/master`
4. Refresh dependencies for Kotlin/Native: `./gradlew `