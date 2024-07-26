# Formal Verification

## Running the plugin

To execute the plugin, build the `dist` target and then
specify the plugin `.jar` with `-Xplugin=`:

```sh
./gradlew dist
dist/kotlinc/bin/kotlinc -language-version 2.0 -Xplugin=dist/kotlinc/lib/formver-compiler-plugin.jar,$HOME/.m2/repository/viper/silicon/1.1-SNAPSHOT/silicon-1.1-SNAPSHOT.jar myfile.kt
```

## Viper dependency

To build the plugin, two main Viper dependencies are needed: 
[silver](https://github.com/viperproject/silver) (the API for
generation the intermediate representation) and 
[silicon](https://github.com/viperproject/silicon) (the symbolic engine for
performing the verification of Viper code).



Therefore, to install the dependencies, be sure to have installed locally:
* The Java Development Kit
* [Maven](https://maven.apache.org/index.html)
* [SBT](https://www.scala-sbt.org/)
* [Z3](https://github.com/Z3Prover/z3) v4.8.7: the best way of installing Z3 with all the necessary would be to
download it from its _Releases page_ [here](https://github.com/Z3Prover/z3/releases/tag/z3-4.8.7).

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

Now, we can clone `silicon` from its GitHub repository and install it in our local Maven repository. 
The compilation process, through the command `sbt assembly`, will create a new fat-JAR file containing `Silicon + Silver` and other Scala dependencies.
Open a new terminal emulator and type
the following commands:

```bash
# The recursive cloning pulls `silver` as well. 
git clone --recursive https://github.com/viperproject/silicon.git
cd silicon
# Compile Scala code into JVM bytecode.
sbt compile
# This command build the fat-JAR file containing all the dependencies
# required by Silver (Silicon, Scala Library, ...)
sbt assembly
# We can now publish the built JAR file into our local Maven repository
mvn install:install-file \
  -Dfile=$(pwd)/target/scala-2.13/silicon.jar \
  -DgroupId=viper -DartifactId=silicon -Dversion=1.1-SNAPSHOT \
  -Dpackaging=jar
```

If everything went well, you should see the following new directory in the local Maven repository: 
`~/.m2/repository/viper/silicon/1.1-SNAPSHOT/silicon-1.1-SNAPSHOT.jar`.

### Common Errors

If you synchronize the Gradle file, and you get an error of non-resolved dependency, create a `~/.m2/settings.xml`
and paste onto it the following content:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <localRepository>${user.home}/.m2/repository</localRepository>
  <offline>false</offline>
</settings>
```

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
