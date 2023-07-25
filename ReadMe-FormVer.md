# Formal Verification

## Running the plugin

To execute the plugin, build the `dist` target and then
specify the plugin `.jar` with `-Xcompiler-plugin=`:

```sh
./gradlew dist
dist/kotlinc/bin/kotlinc -language-version 2.0 -Xcompiler-plugin=dist/kotlinc/lib/formver-compiler-plugin.jar myfile.kt
```

**NOTE:** This currently doesn't work, it's blocked on us making a fat jar
of the plugin.  For now, if you want to run the plugin, use the test suite.

## Viper dependency

To build the plugin, two main Viper dependencies are needed: 
[silver](https://github.com/viperproject/silver) (the API for
generation the intermediate representation) and 
[silicon](https://github.com/viperproject/silicon) (the symbolic engine for
performing the verification of Viper code).

However, the `silicon` *project structure* and *building file* require some
important modifications for allowing Gradle to resolve the dependencies required by both projects.

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
export Z3_EXE /usr/bin/z3 # or a different directory in $PATH
sudo cp z3-4.8.7-*/bin/z3 $Z3_EXE
echo "export Z3_EXE=$Z3_EXE" >> ~/.bashrc
```

Make sure that running `$Z3_EXE --version` gives `Z3 version 4.8.7`.

Now, we can clone `silicon` from its GitHub repository.
Once the cloning process is complete, open `silicon`'s `build.sbt` in your favorite editor
and modify it applying the following [patch](./resources/patches/silicon-edits.patch).

```bash
# Clone the Silicon project in a directory.
# The flag `--recursive` will also clone `silver`.
git clone --recursive https://github.com/viperproject/silicon

cd silicon

# Apply the patch with the needed modifications
git apply silicon-edits.patch
```


Now, open up the `silver`'s building file (located at `./silicon/silver/build.sbt`), and in the _Publishing settings_, 
add the following lines:

```sbt
// Publishing settings
/* ... */
ThisBuild / publishArtifact := true
publishTo := Some(MavenCache("local-maven", file(Path.userHome.absolutePath + "/.m2/repository")))
```

Once done with all the modifications, from the `silicon` root directory we can compile and publish the projects
running `sbt`:

```bash
# Compile and publish silver first
cd silver
sbt compile && sbt publish
# Compile and publish silicon
cd ..
sbt compile && sbt publish
```

If everything went well, you should see the following new directories in the local Maven repository: 
`~/.m2/repository/viper/silicon_2.13` and `~/.m2/repository/viper/silver_2.13`.

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
   via GitHub for `formal-verification`.  This should be a
   fast-forward merge.

Due to the large number of force pushes, it doesn't really work here
to share branches amongst multiple people; if the other person does
a force push, your work will be left in a weird state.

Upstream changes may need to be merged multiple times, as
merging them into `formal-verification` isn't enough to make
them compatible with other branches.  You can work around this
by applying your changes to the new `formal-verification` tip
as a cherry-pick.  TODO: document this process once we've
successfully done it some time.

[0]: https://github.com/JetBrains/kotlin
