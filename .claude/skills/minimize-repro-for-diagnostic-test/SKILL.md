---
name: minimize-repro-for-diagnostic-test
description: Makes a minimal reproduction of a Frontend-related bug as a diagnostic test
user-invocable: true
disable-model-invocation: true
---

# Input

Ask a user for some details, whether it's some YouTrack issue or some locally built user project.
For a local user project, ask for details like where an error had happened, what is the context around, if it's opened in the IDE.

Ask a user whether they need a separate test data file with an initial test version.

# Gather context the context

If a case depends on some dependencies, be it other sources or some libraries, if needed, use their API description on the web.

For a local user project if it's open in the IDE, use JetBrains MCP resolution to determine the context, e.g., by getting symbols info. 

# Initial version

Using information from [tests description](../../../compiler/fir/analysis-tests/AGENTS.md), make a first version of the test which contains
some wide context needed to reproduce the issue, like all _necessary_/referenced declarations dumped (including Java files and dependencies in separate file).

Make sure that the problem is reproduced by running a test, and if it's not, add some more context, if you fail for a couple of times, then stop.

# Minimization

Try to remove or trivialize different parts of the context used for the repro.
If it stops failing for one of them, try doing something under another angle, like trivializing a different part.

# Dump metadata

If it's an inference-related issue, dump the inference logs.


