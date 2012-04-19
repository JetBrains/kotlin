package org.jetbrains.jet.tags;

/**
 * @author abreslav
 */

public class TagData {
    private final TagType type;
    private final String message;
    private final int start;
    private final boolean nextToken;
    private int end;

    public TagData(TagType type, String message, int start, boolean nextToken) {
        this.type = type;
        this.message = message;
        this.start = start;
        this.nextToken = nextToken;
    }

    public TagType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public int getStart() {
        return start;
    }

    public boolean isNextToken() {
        return nextToken;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }
}
