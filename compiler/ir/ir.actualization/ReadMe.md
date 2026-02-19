Actualization is a process of modifying the IR to refer to specific `actual` declarations, instead of `expect` ones.

Frontend representation only references `expect` declarations, and this is how it is translated into IR (by both `psi2ir` and `fir2ir`).
But when we compile for a specific platform, we have to later resolve to its `actual` declarations in the backend - it happens here.