package org.jetbrains.jet.lang.parsing;

/**
 * @author abreslav
 */
public abstract class AbstractTokenStreamPattern implements TokenStreamPattern {

        protected int lastOccurrence = -1;

        @Override
        public int result() {
            return lastOccurrence;
        }
    }

