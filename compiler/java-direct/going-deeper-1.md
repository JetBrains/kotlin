
# Design implementation prompts

## 1

You wrote the general implementation plan for the feature. That's a good starting point. Now let's move one step deeper to the prompting for the actual work.
Let's start with fixing the infrastructure to make sure that the class finder and functionality below it corresponds to the current requirements.
Meanwhile we implemented basic mechanisms that plugs in the new functionality to the compilation and testing pipeline.
The details could be seen in the `JavaClassFinderOverAstFactory` class that passes the `JavaClassFinderOverAstImpl` to the appropriate places.
The newly generated (from the old testdata) tests (located in `compiler/java-direct/build/tests-gen/org/jetbrains/kotlin/java/direct/JavaUsingAstLegacyBoxTestGenerated.java`) directory)
demonstrate that the java files processing is now going via the new `JavaClassFinderOverAstImpl`.
But most of the tests are failing.
So we need to create a plan for fixing most important problems in them. We will not be able to fix all of them without implementing all 
the planned functionality. So we need to start small.

Create a plan in a separate .md document along with the `IMPLEMENTATION_PLAN.md`, containing prompts for agents like Claude 4.5 Sonnet
or Junie, that should instruct it first to understand the root problems that cause most of the failures, then add tests into `compiler/java-direct/test/org/jetbrains/kotlin/java/direct` directory
with the isolated reproducing cases, then try to fix them. Then iterate. The agent must ask confirmation before each iteration. 

Use best practices of prompting to achieve most effective and clean results. 

The agents should use Kotlin repository coding rules and standarts, generate minimal code where possible, and generally produce good and readable code.
The comments explaining the general ideas behind the implementation and the non-trivial or non-obvious solutions are encouraged. But 
excessive commenting is not.

Now continue with analyzing the task and required details from the kotlin project and `direct-java` module, interview me on the missing peaces
and create the prompts as requested.

### 1.1

This looks good contents-wise. But probably non-optimal for the execution. Will it be more beneficial to extract common part of the instructions into a separate file, so the agents could be 
prompted with a reference to the general instructions + current iteration prompt? If you agree with me, please split the document accordingly. Otherwise please explain, why the current form is better. 

### 1.2

How do I incorporate the adjustments to the processes, the knowledge of the subsystems and changes in the implementation so they are accessible for the next iterations, without overloading the agents with 
excessive context? Should we add some instructions for the agens to dump some findings and general results description into a file, so I can prompt, e.g. you, later to update the general instructions?

### 1.3

(fixing commenting style)

## 2 implementation prompts

Read @AGENT_INSTRUCTIONS.md and @FIXING_ITERATIONS.md,
then execute Iteration 1

### 2.1

Good work identifying and fixing the critical bug!

The one issue with the solution is the excessive commenting. I removed the unnecessary comments and added more instructions to the @AGENT_INSTRUCTIONS.md.
Please respect the updated version of the document.
Proceed to Iteration 2: Type Resolution Implementation.
