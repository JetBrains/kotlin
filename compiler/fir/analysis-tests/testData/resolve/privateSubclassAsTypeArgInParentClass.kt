// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-66672

interface TreeTableStateStrategy<T>

class OnModelChangeTreeStateStrategy : TreeTableStateStrategy<OnModelChangeTreeStateStrategy.SelectionState> {
    private class SelectionState
}