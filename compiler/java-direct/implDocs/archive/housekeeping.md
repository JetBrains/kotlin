
# Housekeeping

## context cleanup 1

We are implementing the `java-direct` module according to the developed plan with relative success. But it looks like the context became too big already and stays on the way of effective agents work.
We need to sort out the relevant parts from the already unimportant history, cleanup the former and move the latter to the archive folder. I created a folder in `java-direct` - `implDocs/archive` for this purpose.
The current state is the "Iteration 6" is finished.
Read the documents starting from @file:IMPLEMENTATION_PLAN.md followed by @file:FIXING_ITERATIONS.md  and @ITERATION_RESULTS.md, and also documents mentioned there, when needed. Move already implemented parts of the FIXING_ITERATIONS and ITERATION_RESULTS to the appropriate documents in the archive folder, and replace with the short summaries in the original documents. Link the archived documents in summaries too, but only in the FIXING_ITERATIONS document and with warning that they should be followed only if there is real necessity to restore deep context. Also move the other design documents mentioned in the archived parts but without direct links in the actual parts to the archive folder.
Do not touch uniplemented iterations (starting from 7) yet. And do not touch general agent instructions yet.

## context cleanup 2

We are implementing the `java-direct` module according to the developed plan with relative success. But it looks like the context became too big again and stays in the way of effective agents' work.
We need to sort out the relevant parts from the already unimportant history again, cleanup the former and move the latter to the archive folder. There is a folder in `java-direct` - `implDocs/archive` 
that keeps the archived documents.
The second problem is that the FIXING_ITERATIONS document became obsolete from around iteration 11. 
The current state is the "Iteration 16" is finished.
Read the documents starting from @file:IMPLEMENTATION_PLAN.md followed by @file:FIXING_ITERATIONS.md  and @ITERATION_RESULTS.md, and also documents mentioned there, when needed.
During the reading, collect all general information on what works and what not, and update the @file:AGENT_INSTRUCTIONS.md accordingly, so the important information is not lost. The key here is to extract generic information, 
like this approach to testing works and this not. 
Replace sections of the FIXING_ITERATIONS document starting from 11 with a note that the rest was ad-hoc error analysis driven, and archive it.
Move already implemented parts of the ITERATION_RESULTS to the appropriate documents (e.g. ITERATIONS_7_16.md) in the archive folder, and replace with the short summaries in the original documents. Link the archived documents in summaries too, but only in the FIXING_ITERATIONS document and with warning that they should be followed only if there is real necessity to restore deep context. Also move the other design documents mentioned in the archived parts but without direct links in the actual parts to the archive folder.


