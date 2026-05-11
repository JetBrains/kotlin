## Code authoring guidelines

The goal of this document is to provide general contribution guidelines that serve as **the** reference for simplifying, unifying, and streamlining our code authoring process.

It can and should be used as the reference when expectations differ between the reviewer and the author.

### Code review accountability

* For any code review, there is **a single primary code reviewer** who is solely responsible for the change under review:
  - They review not only the change in their subsystem, but the change as a whole: they decide whether the change is reasonable overall and whether it is future-proof and sustainable in the long run. They know the context of the feature, the final deliverable, and further related enhancements.
  - In effect, this means that the reviewer is next in line to maintain this change, answer other developers’ questions about it, and fix regressions in this change.
  - We emphasize that this means “the code is properly documented and tested, and is approachable and maintainable by others” rather than “If I am responsible for it, then it should be written in the manner I would have done so”.
* `CODEOWNERS` reviewers review the change in their particular subsystems.
  - Their responsibility is to ensure that the change in the corresponding subsystem makes sense: it is consistent with the rest of the subsystem, is tested and/or documented according to their subsystem’s standards, does not contradict potential subsequent changes, etc.
  - There is no requirement to review other subsystems. This is up to the primary reviewer.

There is no formal way to define the primary code reviewer, but it is encouraged to mention them explicitly if it is not obvious. This reviewer should look at the overall picture when reviewing, and when there are multiple reviewers, it is worth spelling out explicitly who the primary reviewer is.
When assigned as a reviewer, your very first action should be to ensure that you are the relevant reviewer for the change and reassign it to someone else otherwise.

### Code authoring

This section contains overall guidance on how to author complex changes.

It is intended to achieve the following goals:
1. Make the CRs as small as possible without slowing the pace of development.
   - Experience has shown many times that this positively affects the speed, predictability, and quality of code review
2. Make the changes well-isolated and easy to review
3. Make the history of the changes self-descriptive, so the reasoning behind the change can be easily understood long after the change is merged

Preparing the MR requires non-zero effort compared with pushing changes “as is”, and cutting corners here is not recommended.
The following list of general recommendations (that can also be used as a reviewer/author checklist) might be particularly helpful:

1. Non-functional changes should be extracted into dedicated commits. This lets them be reviewed separately and excluded from the review using [the Space feature](https://resources.jetbrains.com/help/img/space/mergeRequestDefaultDiff.png)
    * If the review is expected to take a significant amount of time, such refactorings may be, and are encouraged to be, merged separately
    * Non-functional changes include, but are not limited to, manual and automatic refactorings, reformats, and code restructuring
2. If the MR is functional, ensure that at least a single commit explicitly mentions the corresponding YT ticket
3. Unless explicitly stated, if the MR mentions more than one YT ticket, it is recommended to split the MR into multiple ones, one per ticket
4. We explicitly emphasize that the previous recommendation also applies to situations where pre-existing bugs appear during MR development or the changeset uncovers a new series of issues and/or bugs.
    * In such scenarios, please file a separate ticket with its own description, reproducer, fixing MR, and a test suite.
5. Keep the granularity of the change reasonably small, but avoid overdoing it:
    * A few small isolated refactorings can be combined in a single commit with a bullet list of changes, especially if they are automated or trivial;
    * Rule of thumb: if refactorings are trivial and the reviewer is unlikely to return to them after taking an initial look, it’s better to squash them before creating an MR. This keeps the history more concise, makes git annotate/blame easier, and keeps the focus on the actual semantics in both review and history
6. Ensure that the change is covered by automated tests. Pre-existing tests may be enough, or new tests should be added in the MR.
    * We can say that a test covers "X" if reverting "X" makes the test fail.
    * For example, if you revert your whole change (but not the tests), some tests should be failing.
    * The same applies to any non-trivial problem you encountered while developing the change: if a problem made you change the approach or introduce special cases in the implementation, there should be a test that would fail if you didn't.
    * If the change fixes a regression, there should be a test reflecting this regression.
    * When adding a test for a fix, it is particularly helpful to first write the test, make sure it fails as expected, and then proceed to fix it.

### Commit messages and YT tickets

In addition to the content guidelines below, follow this basic commit message style:

* Keep the subject line at 72 characters or less.
* Start the subject with a `[Subsystem]` prefix. Common examples include
  `[FIR]`, `[IR]`, `[Analysis API]`, `[Gradle]`, `[Native]`, `[JS]`,
  `[Wasm]`, `[Tests]`, and `[Docs]`.
* Separate the subject from the body with one blank line.
* Hard-wrap body paragraphs at 72 characters. This matches IntelliJ IDEA's
  commit message inspection and keeps command-line history readable.
* Use separate paragraphs or bullet points for distinct ideas.
* Use imperative mood for the subject when practical.

1. At least one commit in an MR with functional changes should mention a YT ticket
2. Please ensure that the ticket description is **descriptive**: the reviewer should understand from the ticket (not from the changes!) what the final goal of the change is and what is being done. The responsible QA should be able to determine what they should check afterward
3. Please ensure that the commit message is self-descriptive and detailed, explaining not only what’s being done but also how and why it is done this way
    * Rule of thumb: if the MR required a nontrivial discussion, hundreds of lines of production code changed, but the final commit message looks like: “use X instead of Y in Z”, there is probably something wrong with the message.
    * This is also subject to code review
4. Even for non-functional changes, it is strongly encouraged to explain *the reasoning* behind the change. Otherwise, it will be lost over time after a few months
    * Rule of thumb: prefer `Make X lazy: \n it’s a non-trivial computation that is frequently unused in the context of X` over `Make X lazy` or `Add X.toString(), used for a debugging purpose` over `Add X.toString()`
5. Avoid “fixup!” commits in the main branch: either squash such commits manually or ensure that they are properly [autosquashable](https://git-scm.com/docs/git-rebase#Documentation/git-rebase.txt---autosquash)


### Git

With the recommended practices, the history should already be compact and descriptive.

A few specifics apply:

* Avoid force-pushes except for rebase; in that case, let the reviewers know that nothing they reviewed was overwritten
    * In particular, if you need to rebase and add some changes, do this in two pushes: one force-push just for rebase and one regular push for the actual changes.
* When merging, the choice between squash and rebase is left to the author
    * When squashing, ensure that none of the intermediate commit messages (except for fixups) or YT references are lost.

### TODOs

It is well known that once a `TODO` comment is left in the code, it tends to stay there forever (or until someone eventually revisits that code and fixes it). Such TODOs hurt the quality of the codebase for the following reasons:
- Usually, comments in those TODOs are quite brief, and it's hard to understand the original intent without additional context (which is forgotten quite quickly)
- `TODO` in the code means that there is a known problem in the code. Ideally, it should be fixed immediately or tracked using the regular mechanisms

To avoid leaving such TODOs in the code without a clear follow-up, use the following process: if you leave a `TODO` comment or `TODO("some reason")` in code that is going to be pushed to `master`, please:
- Create a YouTrack ticket with a description of what should be done with this `TODO` and why
- Add the `kotlin-todo` tag to this ticket
- Mention this ticket in the title line of the `TODO` itself, for example, `TODO KT-XXXX description`
- (Optional) mention this ticket in the commit where the TODO was introduced
- (Optional) link the specific place in code with the `TODO` in the ticket

As a reviewer, please perform the following actions when you see a newly introduced `TODO` in the code:
- Check whether a ticket is mentioned
- If it is, ensure that the ticket is described well enough to provide the context needed to fix this `TODO` in the future
- If it isn't, please ask the author of the code to add one

- When an issue with `kotlin-todo` is resolved, please ensure that the author has fixed all corresponding TODOs in the code.
