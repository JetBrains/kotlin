package org.jetbrains.konan.analyser.index;

import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.vfilefinder.KotlinFileIndexBase;
import org.jetbrains.kotlin.metadata.KonanLinkData;
import org.jetbrains.kotlin.name.FqName;

public class KonanMetaFileIndex extends KotlinFileIndexBase<KonanMetaFileIndex> {
  private static final int VERSION = 4;

  /*todo: check version?!*/
  private final DataIndexer<FqName, Void, FileContent> INDEXER = indexer(fileContent -> {
    KonanLinkData.LinkDataPackageFragment fragment = KonanDescriptorManager.getINSTANCE().parsePackageFragment(fileContent.getFile());
    return new FqName(fragment.getFqName());
  });

  public KonanMetaFileIndex() {
    super(KonanMetaFileIndex.class);
  }

  @NotNull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return file -> file.getFileType() == KonanMetaFileType.INSTANCE;
  }

  @NotNull
  @Override
  public DataIndexer<FqName, Void, FileContent> getIndexer() {
    return INDEXER;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }
}
