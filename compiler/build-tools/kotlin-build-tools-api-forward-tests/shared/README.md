## Build Tools API forward compatibility integration tests shared module

This module contains only shared test infrastructure parts for the Build Tools API forward compatibility integration tests.

## CAUTION

Do not call/reference BTA APIs from here if possible (currently only a small subset is used).

If in the future compilation fails because of current BTA API usage in this shared module, that means that these APIs
are no longer safe to call from multiple BTA API versions and should be adjusted, or even un-shared and moved to relevant test suites 
in the parent project.
