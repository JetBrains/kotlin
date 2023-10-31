# Formal Verification

## Running the plugin

To execute the plugin in another project, make sure you've installed the plugin to your local
Maven repository (`./gradlew install`) and then use the Gradle plugin in your project.

In `settings.gradle.kts`, configure your Gradle plugin repositories to allow local plugins:

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
```

Then in `build.gradle.kts`, enable the plugin.  Make sure that you also enable the Maven
local repository here: it's necessary to find the standard library for the plugin.

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.255-SNAPSHOT"
    id("org.jetbrains.kotlin.plugin.formver") version "2.0.255-SNAPSHOT"
}

repositories {
    mavenCentral()
    mavenLocal()
}
```

Additionally, make sure you set Kotlin to use K2 and increase the stack size of the Kotlin Daemon:

```kotlin
kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
    // Set stack size to 30mb
    kotlinDaemonJvmArgs = listOf("-Xss30m")
}
```

#### Plugin configuration

Plugin options can be enabled using the `formver` configuration block:

```kotlin
formver {
    setLogLevel("full_viper_dump")
}
```

However, keep in mind that the Viper is dump is provided as an info message: this message will not be shown
unless you run `gradle` with the `--info` flag.

#### Annotations

The plugin provides a number of annotations.
To access these, add a dependency to `kotlin-formver-compiler-plugin.annotations`:

```kotlin
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-formver-compiler-plugin.annotations:2.0.255-SNAPSHOT")
}
```

### Running from the command line

To execute the plugin directly, build the `dist` target and then
specify the plugin `.jar` with `-Xplugin=`:

```sh
./gradlew dist
dist/kotlinc/bin/kotlinc -language-version 2.0 -Xplugin=dist/kotlinc/lib/formver-compiler-plugin.jar myfile.kt
```

The plugin accepts a number of command line options which can be passed via `-P plugin:org.jetbrains.kotlin.formver:OPTION=SETTING`:
- Option `log_level`: permitted values `only_warnings`, `short_viper_dump`, `full_viper_dump` (default: `only_warnings`).
- Option `error_style`: permitted values `user_friendly`, `original_viper` and `both` (default: `user_friendly`).
- Options `conversion_targets_selection` and `verification_targets_selection`: permitted values `no_targets`, `targets_with_contract`, `all_targets` (default: `targets_with_contract`).
- Option `unsupported_feature_behaviour`: permitted values `throw_exception`, `assume_unreachable` (default: `throw_exception`).

## Z3 dependency

The plugin relies on the SMT solver Z3 which needs to be installed manually.
To do so download v4.8.7 from the _Releases page_ [here](https://github.com/Z3Prover/z3/releases/tag/z3-4.8.7).

Viper gives two ways of interfacing with Z3: text-based (using the `z3` binary)
or via the API (using a `.jar`).
At the moment we use the text-based interface, meaning you need to:
- Install the `z3` binary in your path
- Set the `Z3_EXE` environment variable correctly.

One way to do this is as follows:
```bash
export Z3_EXE=/usr/bin/z3 # or a different directory in $PATH
sudo cp z3-4.8.7-*/bin/z3 $Z3_EXE
echo "export Z3_EXE=$Z3_EXE" >> ~/.profile
```

Make sure that running `$Z3_EXE --version` gives `Z3 version 4.8.7`.
Check that this is the case when you open a new shell, too!
You need to (additionally) set `Z3_EXE` in `~/.xprofile` and/or
`~/.bash_profile` depending on your shell, window manager, display
manager, operating system, etc.

## Viper dependency

We provide a bundled library of Silicon with all its dependencies (including Viper) at [our Maven Space Repository](https://jetbrains.team/p/kotlin-formver/packages/maven/maven/viper/silicon_2.13?v=1.2-SNAPSHOT&tab=overview).
No need to install anything for the usage of the plugin.

# Publishing Silicon

Since Silicon and its dependencies are not centrally published we provide them in [our Maven Space Repository](https://jetbrains.team/p/kotlin-formver/packages/maven/maven).
To use the plugin nothing is required except import the library from there.

However, if you want to publish a new version of the Silicon library here is some useful information about that:

As prerequisites, you will need:
* The Java Development Kit
* [Maven](https://maven.apache.org/index.html)
* [SBT](https://www.scala-sbt.org/)

After that clone and build Silicon:
```bash
# The recursive cloning pulls `silver` as well. 
git clone --recursive https://github.com/viperproject/silicon.git
cd silicon
# Compile Scala code into JVM bytecode.
sbt compile
# This command build the fat-JAR file containing all the dependencies
# required by Silver (Silicon, Scala Library, ...)
sbt assembly
```
To publish the Silicon jar to our Space repo we first need to modify the `built.sbt` script of silicon.
For that refer to the [patch file](resources/patches/silicon-publish-maven.patch) that include all necessary changes.

In addition, you need to generate an access token for write access to the repository.
For that you need to create a credential file at `~/.sbt/space-maven.credentials`.
For detailed information of how to do so see the instructions on the [repository site](https://jetbrains.team/p/kotlin-formver/packages/maven/maven)
under `Connect -> Publish` with tool `sbt` selected.
With the drop-down menu you can create a write-access token.

After completing these steps you will be able to publish the Silicon artifact with
```bash
sbt publish
```

Additional Info:
* If you get a 401 response code while publishing, set the Space repository to private access.
  Due to a bug it is currently not possible to publish to public repos.
* If you want to experiment locally you can install Silicon into the local maven repo with `sbt publishM2`


## Debugging builds

Here's a few things to try if your build is mysteriously not working.
(If you discover a new thing that can go wrong, add it to the list!)

- Check whether the issue is in Gradle or in IntelliJ: you can run
  `./gradlew dist` through either, try both and see whether the errors
  are different.
- Intellisense will sometimes be broken.  Don't worry too much about it.
- Make sure you open the root `kotlin` directory if you use IntelliJ.
  The project does some kind of configuration that's necessary for the
  build to work.
- If you get errors about Java 1.6 and 1.7, make sure your `local.properties`
  file contains `kotlin.build.isObsoleteJdkOverrideEnabled=true`.
- If IntelliJ isn't working, try `File -> Repair IDE`.
- If that doesn't help, try `File -> Invalidate Caches`.
- If even that doesn't help, try `git clean -f -x`.

## Git Usage

The Kotlin repository maintains a linear history, preferring
rebases to merges.  We maintain this in our fork, meaning
that all our commits are applied on top of the changes done
upstream. The `master` branch of this repository is kept in
sync with [JetBrains/kotlin][0], and `formal-verification` is
the de facto master branch for our work.  Every team member
then has their own work branch.

### Workflow

To work in this style, ensure that `pull.rebase` is set to
`true`.  Then:

1. To get upstream changes, sync `master` with upstream
   (this can be done on GitHub) and then run `git rebase master formal-verification`.
   You will need to force push to share these changes on GitHub;
   think about whether you may disturb anyone's pull request before
   you do this.
2. To do work, make your own branch `git checkout -b my-work-branch formal-verification`.
3. To update your personal branch, do `git rebase formal-verification my-work-branch`.
   You will need to force push after this.
4. To move changes from a personal branch, make sure your branch is 
   up-to-date with `formal-verification` and make a pull request
   via GitHub for `formal-verification`.  You can choose between rebasing
   your change onto `formal-verification` or squashing your changes into a
   single commit.  On Linux, you can use the `pull_request.sh` script to open a browser tab
   with `formal-verification` as the target; GitHub will by default suggest you
   make a PR against `JetBrains/kotlin:master`, which is *not* correct.

Due to the large number of force pushes, it doesn't really work here
to share branches amongst multiple people; if the other person does
a force push, your work will be left in a weird state.

### Merging upstream

We rebase `upstream/master` onto `formal-verification` on Mondays.
This may involve resolving merge conflicts between upstream and our changes.
If you rebase `formal-verification` into your own branch, you will
typically have to resolve these conflicts again: to avoid this, do 
an interactive rebase and remove any old commits you see, keeping only
those commits that you've added to your branch since it split off
`formal-verification`.  (Normally, rebase handles this for you, but
because `formal-verification` has been rebased it can fail to see that
certain commits from your branch are already present in `formal-verification`.)
This should ensure that only merge conflicts introduced by your branch
have to be merged.

### Code review

When you've finished work on a branch, open a pull request on
GitHub against the `formal-verification` branch in `jesyspa/kotlin`
(**not** `JetBrains/kotlin`! unfortunately that's the default) and
add someone else as a reviewer.  Some tips:
- On Linux, the `pull_request.sh` script will open a tab and set the base branch correctly for you.
- Anyone can do reviews: don't worry about whom you ask.
- That said, if someone is most familiar with the part you're changing, they may be the best choice.
- Keeping your changes small and focused can make reviewing (and merging!) easier.
- Reviewing isn't just about catching bugs: use it as a chance to make
  sure you understand the code and ask for clarification if necessary.
- Don't forget to comment on good things, too!

[0]: https://github.com/JetBrains/kotlin
