package org.jetbrains.jet.tags;

/**
 * @author abreslav
 */

public abstract class TagType {
    private final String tagName;

    protected TagType(String tagName) {
        this.tagName = tagName;
    }

    public abstract void appendOpenTag(StringBuilder builder, TagData tagData);
    public abstract void appendCloseTag(StringBuilder builder, TagData tagData);

    @Override
    public String toString() {
        return tagName;
    }

    public String getTagName() {
        return tagName;
    }
}
