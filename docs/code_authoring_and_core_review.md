## Code authoring guideline

The goal of this document is to describe a general contribution guideline that serves as **the** reference, facilitating the simplification, unification, and streamlining of our code authoring process.

It can and should be used as the reference for the mismatches between the reviewer and the author.

### Code review accountability

* For any code review, there exists **a single primary code reviewer**, solely responsible for the change processed:
  - They review not only the change from their subsystem but the change in general – they decide whether the change is reasonable as a whole and whether it is future-proof and sustainable in the long run. They know the context of the feature, the final deliverable, and further related enhancements.
  - Effectively, it means that this reviewer is the next in line to maintain this change further, answer other developers’ questions regarding the change, and fix regressions in this very change.
  - We emphasize that it means “the code is properly documented and tested, is approachable and maintainable by others” rather than “If I am responsible for it, then it should be written in the manner I would have done so”.
* `CODEOWNERS` reviewers review the change in their particular subsystems
  - Their responsibility is to ensure that the change in the corresponding subsystem makes sense: it is consistent with the rest of the subsystem, is tested and/or documented according to their subsystem’s standards, does not contradict potential subsequent changes, etc.
  - There is no requirement to review other subsystems. This is up to the primary reviewer

There is no formal way to define the primary code reviewer, but it’s encouraged to mention them explicitly if it is not obvious. This reviewer should take a look at the overall picture when reviewing, and when there are multiple reviewers, it is worth explicitly spelling out who is the primary one.
When assigned as a reviewer, your very first action should be to ensure you are the relevant reviewer for the change and re-assign to someone else otherwise.

### Code authoring

This section contains overall guidance on how to author complex changes.

It is aimed to reach the following goals:
1. Make the CRs as small as possible without losing the development pace. 
   - A multitude of times, it has been shown that it positively affects the speed, predictability, and quality of the code review
2. Make the changes well-isolated and easy to review
3. Make the history of the changes self-descriptive, so the reasoning behind the change can be easily understood way past the change

The preparation of the MR requires a non-zero effort compared to pushing “as is”, and it is not recommended to cut corners here.
The following list of general recommendations (that can also be used as a reviewer/author checklist) might be particularly helpful:

1. Non-functional changes should be extracted into dedicated commits. So they can be reviewed separately and excluded from the review using [the Space feature](https://resources.jetbrains.com/help/img/space/mergeRequestDefaultDiff.png)
    * If the review is expected to take a significant amount of time, such refactorings might be and encouraged to be merged separately
    * Non-functional changes include but are not limited to manual and automatic refactorings, reformats, code restructuring
2. If the MR is functional, ensure that at least a single commit explicitly mentions the corresponding YT ticket
3. Unless explicitly specified, if the MR mentions more than one YT ticket, it is recommended to split the MR into multiple ones, one per ticket
4. We explicitly emphasize that the previous recommendation also applies to situations where previously existing bugs manifested themselves during the MR development or the changeset uncovers a new series of issues and/or bugs.
    * In such scenarios, please file a separate ticket with its own description, reproducer, fixing MR, and a test suite.
5. Keep the granularity of the change reasonably small, but avoid overdoing it:
    * A few small isolated refactorings can be fused together in a single commit with a bullet list of changes, especially if they are automated or trivial;
    * Rule of thumb: if refactorings are trivial and the reviewer is unlikely to get back to it after taking an initial look, it’s better to squash them before creating an MR – more concise history, easier git annotate/blame, and focus on the actual semantics both in review and history

### Commit messages and YT tickets

1. At least one commit in an MR with functional changes should mention a YT ticket
2. Please ensure that the ticket description is **descriptive**: the reviewer should understand from the ticket (not from the changes!) what the final goal of the change is and what is being done. The responsible QA should be able to deduce what they should check afterwards
3. Please ensure that the commit message is self-descriptive and detailed, explaining not only what’s being done but also how and why it is done this way
    * Rule of thumb: if the MR required a nontrivial discussion, hundreds of lines of production code changed, but the final commit message looks like: “use X instead of Y in Z”, chances that something is wrong with the message are high.
    * This is also a subject of the code review
4. Even for non-functional changes, it is strongly encouraged to explain *the reasoning* behind the change. Otherwise, it will be lost in time in a few months
    * Rule of thumb: prefer `Make X lazy: \n it’s a non-trivial computation that is frequently unused in the context of X` over `Make X lazy` or `Add X.toString(), used for a debugging purpose` over `Add X.toString()`
5. Avoid “fixup!” commits in the main branch: either squash such commits manually or ensure it is properly [autosquashable](https://git-scm.com/docs/git-rebase#Documentation/git-rebase.txt---autosquash)


### Git

With the recommended practices, the history should already be compact and descriptive.

A few specifics apply:

* Avoid force-pushes except for rebase; in that case, let the reviewers know nothing reviewed was overwritten
* When merging, the choice between a squash and rebase is left to the author
    * When squashing, ensure that none of the intermediate commit messages (except for fixups) or YT references are lost.

