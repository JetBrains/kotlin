package org.jetbrains.jet.lang.resolve.calls.inference;

/**
* @author abreslav
*/
public interface SolutionStatus {
    SolutionStatus SUCCESS = new SolutionStatus() {
        @Override
        public boolean isSuccessful() {
            return true;
        }
    };

    boolean isSuccessful();
}
