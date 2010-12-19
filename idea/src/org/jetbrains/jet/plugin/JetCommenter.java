package org.jetbrains.jet.plugin;

import com.intellij.lang.Commenter;

/**
 * Created by IntelliJ IDEA.
 * User: max
 * Date: 12/19/10
 * Time: 5:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class JetCommenter implements Commenter {
    @Override
    public String getLineCommentPrefix() {
        return "//";
    }

    @Override
    public String getBlockCommentPrefix() {
        return "/*";
    }

    @Override
    public String getBlockCommentSuffix() {
        return "*/";
    }

    @Override
    public String getCommentedBlockCommentPrefix() {
        return null;
    }

    @Override
    public String getCommentedBlockCommentSuffix() {
        return null;
    }
}
