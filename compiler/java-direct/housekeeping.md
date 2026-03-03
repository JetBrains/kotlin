
# Housekeeping

## context cleanup 1

We are implementing the `java-direct` module according to the developed plan with relative success. But it looks like the context became too big already and stays on the way of effective agents work.
We need to sort out the relevant parts from the already unimportant history, cleanup the former and move the latter to the archive folder. I created a folder in `java-direct` - `implDocs/archive` for this purpose.
The current state is the "Iteration 6" is finished.
Read the documents starting from @file:IMPLEMENTATION_PLAN.md followed by @file:FIXING_ITERATIONS.md  and @ITERATION_RESULTS.md, and also documents mentioned there, when needed. Move already implemented parts of the FIXING_ITERATIONS and ITERATION_RESULTS to the appropriate documents in the archive folder, and replace with the short summaries in the original documents. Link the archived documents in summaries too, but only in the FIXING_ITERATIONS document and with warning that they should be followed only if there is real necessity to restore deep context. Also move the other design documents mentioned in the archived parts but without direct links in the actual parts to the archive folder.
Do not touch uniplemented iterations (starting from 7) yet. And do not touch general agent instructions yet.
