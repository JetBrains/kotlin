move-tmp-to-kt:
	rename -f s/.kt.tmp/.kt/ `find "testData" -name "*.kt.tmp"`

remove-tmp:
	rm `find "testData" -name "*.kt.tmp"`

disable-sandbox:
	cd ~/.IdeaIC11/system/ &&	mv plugins-sandbox plugins-sandbox.tmp

enable-sandbox:
	cd ~/.IdeaIC11/system/ && rm -rf plugins-sandbox && mv plugins-sandbox.tmp ./plugins-sandbox

release-patch:
	cp ~/src/jet/.git/patches/master/j2k ~/src/jet-contrib/upstream-patches/