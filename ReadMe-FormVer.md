# Formal Verification

## Running the plugin

To execute the plugin, build the `dist` target and then
specify the plugin `.jar` with `-Xcompiler-plugin=`:

```sh
./gradlew dist
dist/kotlinc/bin/kotlinc -language-version 2.0 -Xcompiler-plugin=dist/kotlinc/lib/formver-compiler-plugin.jar myfile.kt
```

## Viper dependency

To build the plugin, two main Viper dependencies are needed: 
[silver](https://github.com/viperproject/silicon) (the API for
generation the intermediate representation) and 
[silicon](https://github.com/viperproject/silicon) (the symbolic engine for
performing the verification of Viper code).

However, the `silicon` *project structure* and *building file* require some
important modifications for allowing Gradle to resolve the dependencies required by both projects.

Therefore, to install the dependencies, be sure to have installed locally:
* The Java Development Kit
* [Maven](https://maven.apache.org/index.html)
* [SBT](https://www.scala-sbt.org/)
* [Z3](https://github.com/Z3Prover/z3) v4.8.7

The installing of `Z3` is not necessary enough, it is important to have the JAR dependency required by `silicon`.
Thus, open your terminal emulator and download the file (you can use `curl`/`wget`):

```bash
cd /tmp
# Download the Z3 jar file from the following provider (used by silicon)
wget https://www.sosy-lab.org/ivy/org.sosy_lab/javasmt-solver-z3/com.microsoft.z3-4.8.7.jar
# Install the Z3 jar file into the local Maven repository
mvn install:install-file \
   -Dfile=./com.microsoft.z3-4.8.7.jar \
   -DgroupId=com.microsoft \
   -DartifactId=z3 \
   -Dversion=4.8.7 \
   -Dpackaging=jar \
   -DgeneratePom=true
```

Now, we can clone `silicon` from its GitHub repository.Once the cloning process is complete, open `silicon`'s `build.sbt` in your favorite editor
and modify it applying the following [patch](./resources/patches/silicon-edits.patch).

```bash
# Clone the Silicon project in a directory.
# The flag `--recursive` will also clone `silver`.
git clone --recursive https://github.com/viperproject/silicon

cd silicon

# Apply the patch with the needed modifications
git apply silicon-edits.patch
```


Now, open up the `silver`'s builiding file (located at `./silicon/silver/build.sbt`), and in the _Publishing settings_, 
add the following lines:

```sbt
// Publishing settings
/* ... */
ThisBuild / publishArtifact := true
publishTo := Some(MavenCache("local-maven", file(Path.userHome.absolutePath + "/.m2/repository")))
```

Once done with all the modifications, from the `silicon` root directory we can compile and publish the project
running `sbt`:

```bash
sbt compile
sbt publish
```

If everything went good, you should see the following new directories in the local Maven repository: 
`~/.m2/repository/viper/silicon_2.13` and `~/.m2/repository/viper/silver_2.13`.

### Common Errors

If you syncrhonize the Gradle file, and you get an error of non-resolved dependency; create a `~/.m2/settings.xml`
and paste onto it the following content:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <localRepository>${user.home}/.m2/repository</localRepository>
  <offline>false</offline>
</settings>
```

## Git Usage

In order to permit changes to eventually be merged upstream,
we use a rebase-oriented system.  The `master` branch of
this repository is kept in sync with [JetBrains/kotlin][0],
and `formal-verification` is the defacto master branch for
our work.

To work in this style, ensure that `pull.rebase` is set to
`true`.  Then:

1. To get upstream changes, sync `master` with upstream
   (this can be done on GitHub) and then run `git rebase master formal-verification`.
3. To move changes to a personal branch, do `git rebase formal-verification my-work-branch`.
4. To move changes from a personal branch, make a pull request
   via GitHub to `formal-verification`.  This should be a
   fast forward.

Upstream changes may need to be merged multiple times, as
merging them into `formal-verification` isn't enough to make
them compatible with other branches.  It's not clear what we
can do to improve this.

TODO: some of these operations require force pushes, we should
figure out how we'll coordinate those.

[0]: https://github.com/JetBrains/kotlin
