## Intention/QuickFix/Inspection Quick Notes

### Platform

Some general information about writing inspection, intention and quick fixes available in [Code Inspections](
https://www.jetbrains.org/intellij/sdk/docs/tutorials/code_inspections.html) document.

It's important to know about [PSI](https://www.jetbrains.org/intellij/sdk/docs/basics/architectural_overview/psi.html). 

Some PSI viewer will be extremely useful. It might be built-in [Psi Viewer](https://www.jetbrains.com/help/idea/psi-viewer.html) that is 
going to be enabled when testing inspections with `IDEA` run configuration (**idea.is.internal=true** must be set to active it in Kotlin 
project). Alternately, external plugin [PsiViewer](https://plugins.jetbrains.com/plugin/227-psiviewer) can be installed.

### Kotlin Project Specific

- Each inspection should be registered in `plugin-common.xml`. This file is included to `plugin.xml` for all plugins obtained from Kotlin 
source code. 

- Each inspection should be added with the description. The description file should be named after correspondent inspection class with 
adding `*.html` extension and removing `Inspection` suffix. (`Some.html` for inspection class `SomeInspection`).

- All inspections should have automatic tests. A default place for such tests is `inspectionsLocal` group (`inspections` group can be 
used if there are no quick fixes available for the inspection).

- `ProblemHighlightType` should always be `ProblemHighlightType.GENERIC_ERROR_OR_WARNING` or empty (there's a correspondent overload for 
`ProblemsHolder.registerProblem()`), otherwise, it won't be possible to individually change the desired level in the inspection settings.

- Resolve operations (`analyze`, `resolveToCall`, `resolveToDescriptors`) are considered to be expensive and shouldn't be triggered more 
often than it's absolutely needed. All possible checks should be applied on PSI or file text before actual resolve. 

-  `resolveToDescriptorIfAny()` / `resolveToCall()` functions can be used if the single descriptor or resolved call is needed. 
It's better to call `analyze` for obtaining `BindingTrace` and use it for fetching other resolution results when inspection 
requires multiple resolve calls in a row.

- Prefer `resolveToDescriptorIfAny()` over `resolveToDesciptor()` because of exceptions that are thrown from latter 
function when descriptor is absent. 

- There shouldn't be PSI elements stored in QuickFix classes (`val psi: PsiElement`) as such elements might be invalidated and can lead 
to memory leaks. Smart pointer (check `SmartPsiElementPointer` class and `createSmartPointer()` function) can be used when such storage 
is absolutely necessary. 

- It's possible to obtain `LanguageVersionSettings` directly from `psiElement` with `languageVersionSettings` extension.

- Kotlin project itself can be used for new inspection testing. Build a test plugin with the new inspection included, open another 
instance of Kotlin project and run inspection on the whole project (`Run Inspection By Name` action). 
    * Check the execution time and consider performance issues?
    * Check the memory consumption. 
    * Check the found problems for false positive.
    * Try to apply a quick fix and check result.