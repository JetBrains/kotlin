move-tmp-to-kt:
	rename -f s/.kt.tmp/.kt/ `find "testData" -name "*.kt.tmp"`

remove-tmp:
	rm `find "testData" -name "*.kt.tmp"`

disable-sandbox:
	cd ~/.IdeaIC11/system/ &&	mv plugins-sandbox plugins-sandbox.tmp

enable-sandbox:
	cd ~/.IdeaIC11/system/ && mv plugins-sandbox.tmp ./plugins-sandbox