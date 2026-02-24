
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

### 1.4

This going good so far. The @ITERATON_RESULTS.md file contains the latest updated.

Now before we can go to the iteration 3, we need to understand how the resolution will access the FitSession - the only proper handle to access the symbol providers.
This is non-trivial, because the JavaClassFinder is created before the FitSession is created, and in general so far considered to be independent, staying above the FIR pipeline.
To evaluate possible solutions we need to understand the pattern or call tree in which we need to execute the resolution: where the request for a resolved symbol is initiated,
what data is available at this point, and what interfaces are used to call the resolution.
I see the following options to make it work (but this list is not necessarily exhaustive):
1. Create a wrapper around JavaClassFinder, that stores the session and performs the resolution, and use this wrapper in all relevant places in FIR, instead of the original JavaClassFinder. The wrapper could be named SessionAearedJavaClassFinder, and it should store the session to the JavaClassOverAstImpl and related instances, where it is necessary for the resolution.
2. Similar as above, but instead of storing the session in the JavaClassOverAstImpl, the resolution functions could just use the session.
3. Probably the cleanest solution from the FIR pipeline perspective - move all resolution tasks to the FirJavaClass (or its implementation). It is already aware of the session, so it can access available symbols.

Analyze how the resolution of JavaClass related Types is organized and executed in FIR, which solutions are feasible (with the strong preference to the solution 3 from my list) and maybe some other important details or obstacles on the way.
Summarize the findings in a separate markdown file that can be used to instruct the implementation agents to make further iterations.


## 2 implementation prompts

Read @AGENT_INSTRUCTIONS.md and @FIXING_ITERATIONS.md,
then execute Iteration 1

### 2.1

Good work identifying and fixing the critical bug!

The one issue with the solution is the excessive commenting. I removed the unnecessary comments and added more instructions to the @AGENT_INSTRUCTIONS.md.
Please respect the updated version of the document.
Proceed to Iteration 2: Type Resolution Implementation.

#### 2.1.1

Good job!
Now before going to the iteration 3, let's try to analyze the problems with constructors you outlined above. Analyze what may be missing in the implementation, add the test case for it, and try to find the solution.
Request my approval after analyzing the possible problems and implementing the test, but before trying the solution in the code.

#### 2.1.2



